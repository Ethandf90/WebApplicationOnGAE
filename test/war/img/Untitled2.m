% X is your image
im=imread('toronto.png');
im=rgb2gray(im);
[M,N,O] = size(im);
% Assign A as zero
A = zeros(size(im));
% Iterate through X, to assign A

    for i=1:M
       for j=1:N
          if(im(i,j) == 255)   % Assuming uint8, 255 would be white
             A(i,j) = 1;      % Assign 1 to transparent color(white)
          end
       end
    end


imwrite(im,'toronto_tran.png','Alpha',A);